
// NO INCLUDE GUARDS, THE HEADER IS INTENDED FOR MULTIPLE INCLUSION

// Copyright Aleksey Gurtovoy 2001-2006
//
// Distributed under the Boost Software License, Version 1.0. 
// (See accompanying file LICENSE_1_0.txt or copy at 
// http://www.boost.org/LICENSE_1_0.txt)
//
// See http://www.boost.org/libs/mpl for documentation.

// $Id: include_preprocessed.hpp,v 1.2 2009-05-21 17:03:44 pvertenten Exp $
// $Date: 2009-05-21 17:03:44 $
// $Revision: 1.2 $

#include <boost/mpl/aux_/config/preprocessor.hpp>
#include <boost/mpl/aux_/config/workaround.hpp>

#include <boost/preprocessor/cat.hpp>
#include <boost/preprocessor/stringize.hpp>

#if !defined(BOOST_NEEDS_TOKEN_PASTING_OP_FOR_TOKENS_JUXTAPOSING)
#   define AUX778076_HEADER \
    plain/BOOST_MPL_PREPROCESSED_HEADER \
/**/
#else
#   define AUX778076_HEADER \
    BOOST_PP_CAT(plain,/)##BOOST_MPL_PREPROCESSED_HEADER \
/**/
#endif

#if BOOST_WORKAROUND(__IBMCPP__, BOOST_TESTED_AT(700))
#   define AUX778076_INCLUDE_STRING BOOST_PP_STRINGIZE(boost/mpl/set/aux_/preprocessed/AUX778076_HEADER)
#   include AUX778076_INCLUDE_STRING
#   undef AUX778076_INCLUDE_STRING
#else
#   include BOOST_PP_STRINGIZE(boost/mpl/set/aux_/preprocessed/AUX778076_HEADER)
#endif

#   undef AUX778076_HEADER

#undef BOOST_MPL_PREPROCESSED_HEADER
