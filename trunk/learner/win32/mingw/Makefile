CC = win32-gcc

SRCDIR=../../src
INCLUDE = -I$(SRCDIR)/common
CFLAGS = $(INCLUDE)
LIBS = -liberty

BINARIES = bnlearner bnmcmc arcweights
FILES=common/bane common/bane_nb common/banesearch \
      common/data common/fdata common/forest \
      common/format common/nb_selector common/nbmodel \
      common/node common/parent_matrix common/score_hashtable

BUILDDIR=obj
DEPDIR=dep

SOURCES=$(patsubst %,$(SRCDIR)/%.c,$(FILES))
OBJECTS=$(patsubst %,$(BUILDDIR)/%.o,$(FILES))
DEPS=$(patsubst %,$(DEPDIR)/%.d,$(FILES))

BINSOURCES=$(patsubst %,$(SRCDIR)/%.c,$(BINARIES))
BINOBJECTS=$(patsubst %,$(BUILDDIR)/%.o,$(BINARIES))
BINDEPS=$(patsubst %,$(DEPDIR)%.d,$(BINARIES))

BUILD_BINARIES=$(patsubst %,$(BUILDDIR)/%,$(BINARIES))

all: $(BUILD_BINARIES)

$(BUILD_BINARIES): $(OBJECTS) $(BINOBJECTS)
	@for i in $(BINARIES); do \
		echo $(CC) -o $(BUILDDIR)/$$i.exe $(BUILDDIR)/$$i.o $(OBJECTS) $(LIBS); \
		$(CC) -o $(BUILDDIR)/$$i.exe $(BUILDDIR)/$$i.o $(OBJECTS) $(LIBS); \
	done

$(BUILDDIR)/%.o : $(SRCDIR)/%.c
	@mkdir -p $(BUILDDIR)/$(dir $*)
	@mkdir -p $(DEPDIR)/$(dir $*)
	$(CC) $(CFLAGS) -MP -MD -MF $(DEPDIR)/$(dir $*)/$(notdir $*).d \
		-o $@ -c $<

clean:
	rm -rf $(DEPDIR) $(BUILDDIR) $(BINARY) `find . -name "*~" -or -name "*.tab.*" -or -name "*.lex.*"`

distclean: clean
	rm -rf config/config.me config/config.log


-include $(DEPS)
